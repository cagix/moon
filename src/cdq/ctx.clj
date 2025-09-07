(ns cdq.ctx)

(defn- valid-tx? [transaction]
  (vector? transaction))

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn- handle-tx! [tx ctx]
  (assert (valid-tx? tx) (pr-str tx))
  (try
   (do! tx ctx)
   (catch Throwable t
     (throw (ex-info "Error handling transaction" {:transaction tx} t)))))

(defn handle-txs!
  "Handles transactions and returns a flat list of all transactions handled, including nested."
  [ctx transactions]
  (loop [ctx ctx
         txs transactions
         handled []]
    (if (seq txs)
      (let [tx (first txs)]
        (if tx
          (let [new-txs (handle-tx! tx ctx)]
              (recur ctx
                     (concat (or new-txs []) (rest txs))
                     (conj handled tx)))
          (recur ctx (rest txs) handled)))
      handled)))

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
