(ns cdq.entity.temp-modifier.draw)

(defn txs [_ entity _ctx]
  [[:draw/filled-circle
    (:body/position (:entity/body entity))
    0.5
    [0.5 0.5 0.5 0.4]]])
