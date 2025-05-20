(ns cdq.tx.show-modal
  (:require [cdq.ctx.show-modal :as show-modal]))

(defn do! [{:keys [ctx/stage
                   ctx/ui-viewport]} opts]
  (show-modal/do! stage
                  ui-viewport
                  opts))

(comment
 (gdl.application/post-runnable!
  (ctx/handle-txs! [[:tx/show-modal {:title "hey title"
                                     :text "my text"
                                     :button-text "button txt"
                                     :on-click (fn []
                                                 (println "hoho"))}]]))
 )
