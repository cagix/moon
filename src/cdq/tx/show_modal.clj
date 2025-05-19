(ns cdq.tx.show-modal
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.show-modal :as show-modal]))

(defn do! [opts]
  (show-modal/do! {:ctx/stage ctx/stage
                   :ctx/ui-viewport ctx/ui-viewport}
                  opts))

(comment
 (gdl.application/post-runnable!
  (cdq.utils/handle-txs! [[:tx/show-modal {:title "hey title"
                                           :text "my text"
                                           :button-text "button txt"
                                           :on-click (fn []
                                                       (println "hoho"))}]]))
 )
