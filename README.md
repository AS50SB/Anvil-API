# Anvil-API  
请在左上角选择分支版本查看对应mod版本的源代码  
---  
Anvil-API允许使用数据包自定义铁砧修复配方，可在Realease里找到示例数据包，修改其中的json就可以修复物品了  
Anvil-API对铁砧逻辑进行了修改，所以目前有修复不消耗经验的bug  
如果不加载数据包，则Anvil-API不会修改原版修复配方  
mod简称：AA  
Anvil-API以MIT许可证开源  
依赖：Fabric Loader  
      Fabric API
      Minecraft(1.20.1)  
mod包名为eab.api  
mod曾用名anvil-custom-repair  
代码由Deepseek编写  
json示例：  
{  
  "item": "minecraft:diamond_sword",//物品  
  "repair_material": "minecraft:oak_log",//用于修复上方物品的物品  
  "repair_amount": 100,//修复的耐久  
  "material_cost": 1,//每次消耗多少材料  
  "experience_cost": 5//经验扣除  
}  
注：注释仅为介绍方法，实际JSON不允许注释，请将//后面的删除
