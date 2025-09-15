(ns cdq.dev
  (:require [cdq.application :as application]
            [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]))

(defn post-runnable! [f]
  (.postRunnable com.badlogic.gdx.Gdx/app
                 (fn [] (f @application/state))))

(comment
 (post-runnable!
  (fn [ctx]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature
                       {:position [35 73]
                        :creature-property (db/build (:ctx/db ctx) :creatures/dragon-red)
                        :components {:entity/fsm {:fsm :fsms/npc
                                                  :initial-state :npc-sleeping}
                                     :entity/faction :evil}}]])))
 )
