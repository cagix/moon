(ns cdq.entity.line-render.draw)

(defn txs [{:keys [thick? end color]} {:keys [entity/body]} _ctx]
  (let [position (:body/position body)]
    (if thick?
      [[:draw/with-line-width
        4
        [[:draw/line position end color]]]]
      [[:draw/line position end color]])))
