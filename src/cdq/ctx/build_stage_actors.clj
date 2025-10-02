(ns cdq.ctx.build-stage-actors
  (:require [cdq.ctx.create-inventory-window]
            [cdq.ui.action-bar]
            [cdq.ui.dev-menu]
            [cdq.ui.entity-info-window]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.message]
            [cdq.ui.player-state-draw]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(defn create-windows [ctx actor-fns]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors (for [[actor-fn & params] actor-fns]
                   (apply actor-fn ctx params))})

(def actor-fns
  [[cdq.ui.dev-menu/create]
   [cdq.ui.action-bar/create]
   [cdq.ui.hp-mana-bar/create]
   [create-windows [[cdq.ui.entity-info-window/create]
                    [cdq.ctx.create-inventory-window/create]]]
   [cdq.ui.player-state-draw/create]
   [cdq.ui.message/create]])

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (doseq [[actor-fn & params] actor-fns]
    (stage/add! stage (scene2d/build (apply actor-fn ctx params))))
  ctx)
