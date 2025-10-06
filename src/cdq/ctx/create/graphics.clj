(ns cdq.ctx.create.graphics)

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   [impl params]]
  (assoc ctx :ctx/graphics ((requiring-resolve impl) gdx params)))
