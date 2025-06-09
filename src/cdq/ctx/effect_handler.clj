(ns cdq.ctx.effect-handler)

(defmulti do! (fn [[k & _params] _ctx]
                k))
