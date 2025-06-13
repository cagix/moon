(ns gdl.create.g
  (:require gdl.create.graphics
            gdl.create.graphics.extends
            gdl.create.graphics.handle-draws))

(defn do! [ctx opts]
  #_(reduce utils/render*
          initial-context-record
          create-fns)
  (gdl.create.graphics/do! ctx opts))
