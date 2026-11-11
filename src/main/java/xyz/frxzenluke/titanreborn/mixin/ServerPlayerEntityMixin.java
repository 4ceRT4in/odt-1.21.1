package xyz.frxzenluke.titanreborn.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameMode;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.frxzenluke.titanreborn.OneDayTitan;
import xyz.frxzenluke.titanreborn.command.StartCommand;
import xyz.frxzenluke.titanreborn.command.TeamCommand;
import xyz.frxzenluke.titanreborn.handler.ConfigHandler;
import xyz.frxzenluke.titanreborn.handler.GameState;
import xyz.frxzenluke.titanreborn.manager.PlayerManager;
import xyz.frxzenluke.titanreborn.manager.TeamManager;
import xyz.frxzenluke.titanreborn.manager.InviteItem;
import xyz.frxzenluke.titanreborn.manager.ServerScheduler;
import xyz.frxzenluke.titanreborn.manager.states.PlayerState;
import xyz.frxzenluke.titanreborn.manager.states.TeamState;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(at = @At("HEAD"), method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V")
    public void onDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        MinecraftServer server = player.getServer();
        if (ConfigHandler.getState(OneDayTitan.getConfigPath()) == GameState.STARTED) {
            server.getPlayerManager().getPlayerList().forEach(playerAll -> {
                playerAll.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                playerAll.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cThe WorldBorder is shrinking to §b"+ (int) (playerAll.getWorld().getWorldBorder().getSize() - 25) + " §cblocks over the next §b60 §cseconds!"), false);
            });
            for (ServerWorld world : server.getWorlds()) {
                WorldBorder worldBorder = world.getWorldBorder();
                worldBorder.interpolateSize(worldBorder.getSize(), (worldBorder.getSize() - 50), 60 * 1000);
            }
            ServerPlayerEntity killer = getKillerFromDamageSource(source);
            if (killer == null) {
                killer = getKillerFromDamageTracker(player);
            }
            if (killer != null) {
                ServerPlayerEntity finalKiller = killer;
                server.getPlayerManager().getPlayerList().forEach(playerAll -> {
                    playerAll.sendMessage(Text.of(OneDayTitan.getPrefix() + "§5" + finalKiller.getName().getString() + " §bhas killed §5" + player.getName().getString() + "§b."), false);
                });
                finalKiller.sendMessage(Text.of("§8✕ §b§l+3 Level"), true);
                finalKiller.addExperienceLevels(3);
            } else {
                server.getPlayerManager().getPlayerList().forEach(playerAll -> {
                    playerAll.sendMessage(Text.of(OneDayTitan.getPrefix() + "§5" + player.getName().getString() + " §bdied."), false);
                });
            }
            ServerScheduler.shared().schedule(server, () -> player.changeGameMode(GameMode.SPECTATOR), 1, TimeUnit.SECONDS);
            PlayerManager.getInstance().setPlayerState(player.getUuid(), PlayerState.DEAD);
            player.sendMessage(Text.of(OneDayTitan.getPrefix() + "§cYou're now a spectator!"), false);
            TeamManager teamManager = TeamManager.getInstance();
            String teamKey = getTeamKeyByPlayer(player.getUuid(), teamManager);
            if (teamKey != null) {
                List<String> playersInTeam = teamManager.getPlayersInTeam(teamKey);
                boolean allDead = playersInTeam.stream()
                        .map(UUID::fromString)
                        .allMatch(uuid -> PlayerManager.getInstance().getPlayerState(uuid) == PlayerState.DEAD);
                if (allDead) {
                    teamManager.setTeamState(teamKey, TeamState.DEAD);
                    checkForWinningTeam(teamManager);
                }
            }
        }
    }
    @Unique
    private String getTeamKeyByPlayer(UUID playerUUID, TeamManager teamManager) {
        for (String teamKey : teamManager.getAllTeams()) {
            List<String> playersInTeam = teamManager.getPlayersInTeam(teamKey);
            if (playersInTeam.contains(playerUUID.toString())) {
                return teamKey;
            }
        }
        return null;
    }

    @Unique
    private void checkForWinningTeam(TeamManager teamManager) {
        Set<String> allTeams = teamManager.getAllTeams();
        long aliveTeamsCount = allTeams.stream()
                .filter(teamKey -> teamManager.getTeamState(teamKey) == TeamState.ALIVE)
                .count();
        if (aliveTeamsCount == 1) {
            String winningTeamKey = allTeams.stream()
                    .filter(teamKey -> teamManager.getTeamState(teamKey) == TeamState.ALIVE)
                    .findFirst()
                    .orElse(null);

            if (winningTeamKey != null) {
                List<String> winningPlayers = teamManager.getPlayersInTeam(winningTeamKey);
                String player1Name = getPlayerNameByUUID(UUID.fromString(winningPlayers.get(0)), OneDayTitan.getServerInstance());
                String player2Name = (winningPlayers.size() > 1) ? getPlayerNameByUUID(UUID.fromString(winningPlayers.get(1)), OneDayTitan.getServerInstance()) : "solo";
                String victoryMessage = (winningPlayers.size() > 1) ?
                        OneDayTitan.getPrefix() + "§5" + player1Name + " §band §5" + player2Name + " §bhave won this §5OneDayTitan§b! Congrats!" :
                        OneDayTitan.getPrefix() + "§5" + player1Name +" §bhas won this §5OneDayTitan§b! Congrats!";

                OneDayTitan.getServerInstance().getPlayerManager().getPlayerList().forEach(p -> {
                    p.sendMessage(Text.of(victoryMessage), false);
                    p.playSoundToPlayer(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 2.0F, 1.0F);
                });
            }
        }
    }
    @Unique
    private String getPlayerNameByUUID(UUID uuid, MinecraftServer server) {
        GameProfile profile = server.getUserCache().getByUuid(uuid).orElse(null);
        return (profile != null) ? profile.getName() : "Unknown Player";
    }
    @Unique
    private ServerPlayerEntity getKillerFromDamageTracker(ServerPlayerEntity player) {
        Entity attacker = player.getPrimeAdversary();
        return attacker == null ? null : getPlayerFromEntity(attacker);
    }
    @Unique
    private ServerPlayerEntity getKillerFromDamageSource(DamageSource source) {
        Entity attacker = source.getAttacker();
        Entity sourceEntity = source.getSource();
        if (attacker instanceof ServerPlayerEntity) {
            return (ServerPlayerEntity) attacker;
        }
        ServerPlayerEntity killer = null;
        if (attacker != null) {
            killer = getPlayerFromEntity(attacker);
            if (killer != null) {
                return killer;
            }
        }
        if (sourceEntity != null && sourceEntity != attacker) {
            killer = getPlayerFromEntity(sourceEntity);
            if (killer != null) {
                return killer;
            }
        }
        return null;
    }

    @Unique
    private ServerPlayerEntity getPlayerFromEntity(Entity entity) {
        if (entity instanceof ServerPlayerEntity) {
            return (ServerPlayerEntity) entity;
        } else if (entity instanceof ProjectileEntity) {
            Entity owner = ((ProjectileEntity) entity).getOwner();
            if (owner instanceof ServerPlayerEntity) {
                return (ServerPlayerEntity) owner;
            }
        } else if (entity instanceof TameableEntity) {
            Entity owner = ((TameableEntity) entity).getOwner();
            if (owner instanceof ServerPlayerEntity) {
                return (ServerPlayerEntity) owner;
            }
        } else if (entity instanceof AreaEffectCloudEntity) {
            Entity owner = ((AreaEffectCloudEntity) entity).getOwner();
            if (owner instanceof ServerPlayerEntity) {
                return (ServerPlayerEntity) owner;
            }
        }
        return null;
    }

    @Inject(at = @At("HEAD"), method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", cancellable = true)
    public void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (ConfigHandler.getState(OneDayTitan.getConfigPath()) != GameState.STARTED || StartCommand.protectionTime) {
            cir.setReturnValue(false);
        }
    }

    @Inject(at = @At("HEAD"), method = "attack(Lnet/minecraft/entity/Entity;)V", cancellable = true)
    public void attack(Entity target, CallbackInfo ci) {
        if (ConfigHandler.getState(OneDayTitan.getConfigPath()) != GameState.STARTED) {
            if (target instanceof ServerPlayerEntity) {
                ServerPlayerEntity attackedPlayer = (ServerPlayerEntity) target;
                ServerPlayerEntity attacker = (ServerPlayerEntity) (Object) this;
                ItemStack heldItem = attacker.getMainHandStack();
                UUID inviterUuid = TeamCommand.getInviterForPlayer(attacker.getUuid());
                if (InviteItem.isInviteSword(heldItem)) {
                    if (inviterUuid != null && inviterUuid.equals(attackedPlayer.getUuid())) {
                        executeAcceptCommand(attacker, attackedPlayer);
                    } else {
                        executeInviteCommand(attacker, attackedPlayer);
                    }
                }
            }
            ci.cancel();
        }
    }
    @Unique
    private void executeInviteCommand(ServerPlayerEntity inviter, ServerPlayerEntity invitee) {
        String targetName = invitee.getName().getString();
        String command = "/tteam invite " + targetName;
        Objects.requireNonNull(inviter.getServer()).getCommandManager().executeWithPrefix(inviter.getCommandSource(), command);
    }
    @Unique
    private void executeAcceptCommand(ServerPlayerEntity invitee, ServerPlayerEntity inviter) {
        String inviterName = inviter.getName().getString();
        String command = "/tteam accept " + inviterName;
        Objects.requireNonNull(invitee.getServer()).getCommandManager().executeWithPrefix(invitee.getCommandSource(), command);
    }
}
