package li.cil.cc.common

import li.cil.cc.integration.Integration

class CommonProxy {
  def init(): Unit = {
    Integration.init()
  }

  def postInit() {}
}
