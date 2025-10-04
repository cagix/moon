(ns cdq.ctx.create.graphics)

(defn do!
  [{:keys [ctx/files
           ctx/graphics]
    :as ctx}
   [impl params]]
  (assoc ctx :ctx/graphics ((requiring-resolve impl) graphics files params)))
