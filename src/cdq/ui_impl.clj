(ns cdq.ui-impl
  (:require [cdq.ui :as ui]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [clojure.graphics.viewport :as viewport]
            [clojure.scene2d.vis-ui.window :as window]
            [com.badlogic.gdx.scenes.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [com.badlogic.gdx.scenes.scene2d.ui.button :as button])
  (:import (com.badlogic.gdx.scenes.scene2d CtxStage)))

(extend-type CtxStage
  ui/Stage
  (mouseover-actor [this position]
    (stage/hit this
               (viewport/unproject (stage/viewport this) position)))

  (actor-information [_ actor]
    (let [inventory-slot (inventory-window/cell-with-item? actor)]
      (cond
       inventory-slot            [:mouseover-actor/inventory-cell inventory-slot]
       (window/title-bar? actor) [:mouseover-actor/window-title-bar]
       (button/is?        actor) [:mouseover-actor/button]
       :else                     [:mouseover-actor/unspecified])))

  (action-bar-selected-skill [this]
    (-> this
        stage/root
        (group/find-actor "cdq.ui.action-bar")
        action-bar/selected-skill)))
