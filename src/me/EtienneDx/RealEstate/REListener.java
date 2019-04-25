package me.EtienneDx.RealEstate;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.PluginManager;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class REListener implements Listener
{
	void registerEvents()
	{
		PluginManager pm = RealEstate.instance.getServer().getPluginManager();
		
		pm.registerEvents(this, RealEstate.instance);
	}
	
	@EventHandler
	public void onSignChange(SignChangeEvent event)
	{
		if(RealEstate.instance.dataStore.cfgSigns.contains(event.getLine(0).toLowerCase()))
		{
			Player player = event.getPlayer();
			Location loc = event.getBlock().getLocation();
			
			Claim claim = GriefPrevention.instance.dataStore.getClaimAt(loc, false, null);
			if(claim == null)// must have something to sell
			{
				player.sendMessage(RealEstate.instance.dataStore.chatPrefix + ChatColor.RED + "The sign you placed is not inside a claim!");
                event.setCancelled(true);
                return;
			}
			if(RealEstate.transactionsStore.anyTransaction(claim))
			{
				player.sendMessage(RealEstate.instance.dataStore.chatPrefix + ChatColor.RED + "This claim is already to sell/lease!");
                event.setCancelled(true);
                return;
			}
			
			// empty is considered a wish to sell
			if(event.getLine(1).isEmpty() || RealEstate.instance.dataStore.cfgSellKeywords.contains(event.getLine(1).toLowerCase()))
			{
				String type = claim.parent == null ? "claim" : "subclaim";
				if(!RealEstate.perms.has(player, "realestate." + type + ".sell"))
				{
					player.sendMessage(RealEstate.instance.dataStore.chatPrefix + ChatColor.RED + "You don't have the permission to sell " + type + "s!");
	                event.setCancelled(true);
	                return;
				}
				if(event.getLine(2).isEmpty())// if no price precised, make it the default one
				{
					event.setLine(2, Double.toString(RealEstate.instance.dataStore.cfgPriceSellPerBlock * claim.getArea()));
				}
				// check for a valid price
				double price;
				try
				{
					price = Double.parseDouble(event.getLine(2));
				}
				catch (NumberFormatException e)
				{
	                player.sendMessage(RealEstate.instance.dataStore.chatPrefix + ChatColor.RED + "The price you entered is not a valid number!");
	                event.setCancelled(true);
	                return;
				}
				if(price <= 0)
				{
	                player.sendMessage(RealEstate.instance.dataStore.chatPrefix + ChatColor.RED + "The price must be greater than 0!");
	                event.setCancelled(true);
	                return;
				}
				
				if(claim.isAdminClaim() && !RealEstate.perms.has(player, "realestate.admin"))// admin may sell admin claims
				{
	                player.sendMessage(RealEstate.instance.dataStore.chatPrefix + ChatColor.RED + "You don't have the permission to sell admin claims!");
	                event.setCancelled(true);
	                return;
				}
				else if(type.equals("claim") && !player.getName().equalsIgnoreCase(claim.getOwnerName()))// only the owner may sell his claim
				{
	                player.sendMessage(RealEstate.instance.dataStore.chatPrefix + ChatColor.RED + "You can only sell claims you own!");
	                event.setCancelled(true);
	                return;
				}
				
				// we should be good to sell it now
				
				RealEstate.transactionsStore.sell(claim, player, price, (Sign)event.getBlock());
			}
		}
	}
}
