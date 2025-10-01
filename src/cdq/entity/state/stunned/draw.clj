(ns cdq.entity.state.stunned.draw)

(defn txs [_ {:keys [entity/body]} _ctx]
  [[:draw/circle
    (:body/position body)
    0.5
    [1 1 1 0.6]]])
