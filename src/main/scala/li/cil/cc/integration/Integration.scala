package li.cil.cc.integration

import cpw.mods.fml.common.Loader

import scala.language.postfixOps

object Integration {
  val mods = Array(
    ("Minecraft", () => Vanilla),
    ("NotEnoughItems", () => NEI)
  )

  def init(): Unit = {
    for ((id, mod) <- mods) {
      if (Loader.isModLoaded(id) || id == "Minecraft") {
        mod().init()
      }
    }
  }

  trait Mod {
    def init(): Unit
  }

}
