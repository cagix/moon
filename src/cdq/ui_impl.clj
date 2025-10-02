(ns cdq.ui-impl
  (:require cdq.ctx.build-stage-actors
            [cdq.ui :as ui]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [clojure.graphics.viewport :as viewport]
            [clojure.scene2d.vis-ui.window :as window]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [com.badlogic.gdx.scenes.scene2d.ui.button :as button])
  (:import (com.badlogic.gdx.scenes.scene2d CtxStage)))

(extend-type CtxStage
  ui/Stage
  (viewport-width  [stage] (:viewport/width  (stage/viewport stage)))
  (viewport-height [stage] (:viewport/height (stage/viewport stage)))

  (get-ctx [this]
    (stage/get-ctx this))

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
        action-bar/selected-skill))

  (rebuild-actors! [this ctx]
    (stage/clear! this)
    (cdq.ctx.build-stage-actors/do! ctx))

  ui/DataViewer
  (show-data-viewer! [this data]
    (stage/add! this (scene2d/build
                       {:actor/type :actor.type/data-viewer
                        :title "Data View"
                        :data data
                        :width 500
                        :height 500})))
  )
