(ns dev.post
  (:require [cdq.application :as application]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [gdl.application]))

(defn post-runnable! [f]
  (gdl.application/post-runnable! (:ctx/app @application/state)
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
