(ns cdq.graphics)

(defn handle-draws!
  [{:keys [ctx/draw-fns]
    :as ctx}
   draws]
  (doseq [{k 0 :as component} draws
          :when component]
    (apply (draw-fns k) ctx (rest component))))
