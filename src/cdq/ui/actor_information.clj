(ns cdq.ui.actor-information
  (:require [cdq.ui.inventory :as inventory-window]
            [clojure.scene2d.vis-ui.window :as window]
            [com.badlogic.gdx.scenes.scene2d.ui.button :as button]))

(extend-type com.badlogic.gdx.scenes.scene2d.CtxStage
  ui/ActorInformation
  (actor-information [_ actor]
    (let [inventory-slot (inventory-window/cell-with-item? actor)]
      (cond
       inventory-slot            [:mouseover-actor/inventory-cell inventory-slot]
       (window/title-bar? actor) [:mouseover-actor/window-title-bar]
       (button/is?        actor) [:mouseover-actor/button]
       :else                     [:mouseover-actor/unspecified]))))
