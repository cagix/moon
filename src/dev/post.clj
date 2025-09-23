(ns dev.post
  (:require [cdq.application :refer [state]]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [clojure.application :as application]))

(defn post-runnable! [f]
  (application/post-runnable! (:ctx/app @state)
                              (fn [] (f @state))))

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
