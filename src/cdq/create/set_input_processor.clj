(ns cdq.create.set-input-processor
  (:require [gdl.input :as input]))

(defn do! [ctx]
  (input/set-processor! (:ctx/input ctx)
                        (:ctx/stage ctx))
  ctx)
