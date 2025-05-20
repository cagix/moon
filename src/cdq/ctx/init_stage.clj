(ns cdq.ctx.init-stage
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.input :as input]
            [gdl.ui :as ui]))

(defn do! [actors]
  (bind-root #'ctx/stage (ui/stage (:java-object ctx/ui-viewport)
                                   (:java-object ctx/batch)
                                   actors))
  (input/set-processor! ctx/stage))
