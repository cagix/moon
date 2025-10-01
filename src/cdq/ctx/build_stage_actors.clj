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

(defn- build-stage-actors!
  [{:keys [ctx/db
           ctx/graphics
           ctx/stage]
    :as ctx}]
  (let [actors [(cdq.ui.dev-menu/create db graphics build-stage-actors!)
                (cdq.ui.action-bar/create)
                (cdq.ui.hp-mana-bar/create stage graphics)
                {:actor/type :actor.type/group
                 :actor/name "cdq.ui.windows"
                 :group/actors [(cdq.ui.entity-info-window/create stage)
                                (cdq.ctx.create-inventory-window/create stage graphics)]}
                (cdq.ui.player-state-draw/create)
                (cdq.ui.message/create)]]
    (doseq [actor actors]
      (stage/add! stage (scene2d/build actor))))
  ctx)

(def do! build-stage-actors!)
