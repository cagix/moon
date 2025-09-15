(ns cdq.create.dissoc-gdx)

(defn do! [ctx]
  (dissoc ctx :ctx/gdx))
