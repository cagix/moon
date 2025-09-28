(ns dev.scratch
  (:require [cdq.application :refer [state]]
            [cdq.ctx :as ctx]
            [cdq.db :as db]))

(comment

 (.postRunnable com.badlogic.gdx.Gdx/app
  (fn []
    (let [{:keys [ctx/db]
           :as ctx} @state]
      (ctx/handle-txs! ctx
                       [[:tx/spawn-creature
                         {:position [35 73]
                          :creature-property (db/build db :creatures/dragon-red)
                          :components {:entity/fsm {:fsm :fsms/npc
                                                    :initial-state :npc-sleeping}
                                       :entity/faction :evil}}]]))))
 )
