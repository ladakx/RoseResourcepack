package me.emsockz.roserp.commands.sub;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.commands.SubCommandModel;
import me.emsockz.roserp.file.config.MessagesCFG;
import me.emsockz.roserp.pack.Applier;
import me.emsockz.roserp.pack.Pack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TextureCMD extends SubCommandModel {

    public static boolean enable = true;

    public TextureCMD() {
        super();
        this.setPlayerCommand(false);
    }


    public boolean execute() {
        if (!this.checkPermission("roserp.commands.texture", true)) {
            return true;
        }

        Player targetPlayer;
        boolean targetPlayerUsed = false;
        if (this.args.length == 1) {
            // Команда без указания игрока, сбрасываем пакеты для отправителя
            if (this.player == null) {
                this.sendMessage(MessagesCFG.NOT_FOR_CONSOLE);
                return true;
            }

            targetPlayer = this.player; // Игрок, который ввел команду
        }

        else if (this.args.length >= 2) {
            // Команда с указанием игрока
            if (Bukkit.getPlayer(this.args[1]) == null) {
                if (args.length != 2) {
                    this.sendMessage(MessagesCFG.PLAYER_NOT_FOUND, "{player}", args[1]);
                }
            }

            targetPlayer = Bukkit.getPlayer(this.args[1]);
            targetPlayerUsed = targetPlayer != null;

            if (targetPlayerUsed) {
                // Проверяем, есть ли у отправителя права на сброс пакетов для другого игрока
                if (!this.checkPermission("roserp.commands.texture.other", true)) {
                    return true;
                }
            } else {
                targetPlayer = player;
            }

        } else {
            // Неверное количество аргументов
            sendHelp();
            return true;
        }

        // Если есть дополнительные аргументы, рассматриваем их как пакеты
        int start = (targetPlayerUsed) ? 2 : 1;
        if (this.args.length > start) {
            List<Pack> packs = new ArrayList<>();

            for (int i = start; i < this.args.length; i++) {
                String packName = this.args[i];

                // Проверка, существует ли пак с данным именем
                if (!RoseRP.hasPack(packName)) {
                    this.sendMessage(MessagesCFG.PACK_NOT_FOUND, "{pack}", packName);
                    continue; // Пропускаем несуществующий пак
                }

                Pack pack = RoseRP.getPack(packName);
                packs.add(pack); // Добавляем пак в список
            }

            // Сбрасываем все собранные паки для целевого игрока
            if (!packs.isEmpty()) {
                Applier.apply(targetPlayer, packs);
                if (targetPlayerUsed) {
                    MessagesCFG.APPLY_OTHER_RESOURCEPACKS.sendMessage(sender, "{player}", targetPlayer.getName());
                }
            }
        } else {
            sendHelp();
        }

        return true;
    }
}
