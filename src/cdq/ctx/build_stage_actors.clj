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

(defn do!
  [{:keys [ctx/stage]
    :as ctx}]
  (let [actors [(cdq.ui.dev-menu/create ctx)
                (cdq.ui.action-bar/create ctx)
                (cdq.ui.hp-mana-bar/create ctx)
                {:actor/type :actor.type/group
                 :actor/name "cdq.ui.windows"
                 :group/actors [(cdq.ui.entity-info-window/create ctx)
                                (cdq.ctx.create-inventory-window/create ctx)]}
                (cdq.ui.player-state-draw/create ctx)
                (cdq.ui.message/create ctx)]]
    (doseq [actor actors]
      (stage/add! stage (scene2d/build actor))))
  ctx)
