package com.valorin.commands.sub;

import static com.valorin.Main.getInstance;
import static com.valorin.configuration.languagefile.MessageSender.gm;
import static com.valorin.configuration.languagefile.MessageSender.sm;

import java.util.List;

import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.valorin.Main;
import com.valorin.caches.EnergyCache;
import com.valorin.commands.SubCommand;
import com.valorin.commands.way.InServerCommand;
import com.valorin.configuration.ConfigManager;
import com.valorin.itemstack.PlayerItems;
import com.valorin.request.RequestsHandler;
import com.valorin.specialtext.ClickableText;
import com.valorin.specialtext.Dec;
import com.valorin.timetable.TimeChecker;
import com.valorin.util.ItemGiver;
import com.valorin.util.ItemTaker;

public class CMDRequestSendAll extends SubCommand implements InServerCommand {

	public CMDRequestSendAll() {
		super("sendall", "sa");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		Player playerSending = (Player) sender;
		String playerSendingName = playerSending.getName();
		if (args.length == 1) {
			if (getInstance().getArenaManager().isPlayerBusy(playerSending.getName())) {// OP比赛时输入
				return true;
			}
			List<String> blackList = Main.getInstance().getCacheHandler().getBlacklist().get();
			if (blackList.contains(playerSendingName)) {
				sm("&c[x]您已被禁赛！", playerSending);
				return true;
			}
			if (!TimeChecker.isInTheTime(playerSending, false)) {
				sm("&c[x]此时间段不开放邀请赛功能，输入/dt timetable查看", playerSending);
				return true;
			}
			EnergyCache cache = Main.getInstance().getCacheHandler().getEnergy();
			if (cache.isEnable()) {
				if (cache.get(playerSending.getName()) < cache.getEnergyNeeded()) {
					sm("&c[x]你的精力值不足！请休息一会", playerSending);
					return true;
				}
			}
			ItemTaker ic = new ItemTaker(playerSending, "§4 §1 §1 §4 §2 §0 §9 §1 §1 §5 §2", 1);
			if (ic.getSlot() == -1) {
				sm("&c[x]本操作需要消耗一个全服邀请函", playerSending);
			} else {
				ic.consume(playerSending);
				sendAll(playerSending);
			}
		} else {
			if (!sender.hasPermission("dt.admin")) {
				sm("&c[x]无权限！", playerSending);
				return true;
			}
			if (args[1].equalsIgnoreCase("getitem")) {
				new ItemGiver(playerSending, PlayerItems.getInvitation(playerSending));
				return true;
			}
		}
		return true;
	}

	private void sendAll(Player playerSending) {
		String sn = playerSending.getName();
		TextComponent txt = ClickableText.sendInvitationToAll(sn);
		int count = 0;
		for (Player receiver : Bukkit.getOnlinePlayers()) {
			if (receiver.getUniqueId().equals(playerSending.getUniqueId())) { // 跳过自己
				continue;
			}
			String rn = receiver.getName();
			List<String> blist = getInstance().getCacheHandler().getBlacklist()
					.get();
			if (blist.contains(rn)) { // 如果对方处于黑名单中，跳过
				continue;
			}
			ConfigManager configManager = getInstance().getConfigManager();
			if (configManager.isWorldWhitelistEnabled()) {
				List<String> worldlist = configManager.getWorldWhitelist();
				if (worldlist != null) {
					if (!worldlist.contains(receiver.getWorld().getName())) {// 如果对方不处于白名单世界中，跳过
						continue;
					}
				}
			}
			if (getInstance().getArenaManager().isPlayerBusy(rn)) {// 对方正在比赛，不可以发送
				continue;
			}
			RequestsHandler rh = getInstance().getRequestsHandler();
			if (rh.getReceivers(sn).contains(rn)) { // 已发送过申请了
				continue;
			}
			Dec.sm(receiver, 2);
			receiver.sendMessage(Dec.getStr(7)
					+ gm("&e玩家&7{player}&e向全服玩家下了单挑战书", null, "player",
							new String[] { sn }));
			Dec.sm(receiver, 0);
			receiver.spigot().sendMessage(txt);
			Dec.sm(receiver, 0);
			Dec.sm(receiver, 2);
			rh.addRequest(sn, rn, null); // 发送申请
			count++;
		}
		sm("&a[v]已有{amount}个玩家收到了你的单挑请求，请等待接受", playerSending, "amount",
				new String[] { "" + count });
	}
}
