(ns dev.scratch
  (:require [cdq.application :refer [state]]
            [cdq.ctx.handle-txs :as handle-txs]
            [cdq.db :as db]
            [com.badlogic.gdx :as gdx]))

(comment

 (gdx/post-runnable!
  (fn []
    (let [{:keys [ctx/db]
           :as ctx} @state]
      (handle-txs/do! ctx
                       [[:tx/spawn-creature
                         {:position [35 73]
                          :creature-property (db/build db :creatures/dragon-red)
                          :components {:entity/fsm {:fsm :fsms/npc
                                                    :initial-state :npc-sleeping}
                                       :entity/faction :evil}}]]))))
 )
