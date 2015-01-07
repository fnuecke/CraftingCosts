package li.cil.cc.integration

import java.lang.reflect.Modifier

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage
import li.cil.cc.CraftingCosts
import li.cil.cc.CraftingCosts.ItemTransformation

import scala.collection.convert.WrapAsScala._
import scala.language.postfixOps

object Integration {
  type JavaRecipeProvider = java.util.concurrent.Callable[java.lang.Iterable[Array[Array[Object]]]]

  type ScalaRecipeProvider = () => Iterable[ItemTransformation]

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

  def process(message: IMCMessage): Unit = {
    if (message.key == "addRecipeProvider") {
      CraftingCosts.log.info(s"Registering recipe provider '${message.getStringValue}' from mod ${message.getSender}.")
      // Try scala callback first.
      try CraftingCosts.addRecipeProvider(tryInvokeStatic[ScalaRecipeProvider](message.getStringValue)()) catch {
        case _: Throwable =>
          // When that fails, try Java one and transform the returned value.
          try {
            val provider = tryInvokeStatic[JavaRecipeProvider](message.getStringValue)()
            CraftingCosts.addRecipeProvider(() => {
              val recipes = (provider: JavaRecipeProvider).call()
              recipes.map {
                case Array(inputs, outputs) =>
                  (inputs.toIterable, outputs.toIterable)
                case _ =>
                  CraftingCosts.log.warn(s"Got a badly formatted recipe via '${message.key}' from mod ${message.getSender}")
                  null
              }.filter(null !=)
            })
          }
          catch {
            case t: Throwable =>
              CraftingCosts.log.warn(s"Error registering recipe provider '${message.key}' from mod ${message.getSender}.")
          }
      }
    }
    else {
      CraftingCosts.log.warn(s"Received unrecognized IMC message of type '${message.key} from mod ${message.getSender}.")
    }
  }

  private def tryInvokeStatic[T](name: String, signature: Class[_]*)(args: AnyRef*): T = {
    val nameSplit = name.lastIndexOf('.')
    val className = name.substring(0, nameSplit)
    val methodName = name.substring(nameSplit + 1)
    val clazz = Class.forName(className)
    val method = clazz.getDeclaredMethod(methodName, signature: _*)
    if (!Modifier.isStatic(method.getModifiers)) throw new IllegalArgumentException(s"Method $name is not static.")
    method.invoke(null, args: _*).asInstanceOf[T]
  }

  trait Mod {
    def init(): Unit
  }

}
