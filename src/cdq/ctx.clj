(ns cdq.ctx)

(defprotocol TransactionHandler
  (handle-txs! [_ transactions]))

(defn draw!
  [{k 0 :as component}
   {:keys [ctx/draw-fns]
    :as ctx}]
  (let [draw-fn (draw-fns k)]
    (draw-fn component ctx)))

(defn handle-draws! [ctx draws]
  (doseq [component draws
          :when component]
    (draw! component ctx)))
