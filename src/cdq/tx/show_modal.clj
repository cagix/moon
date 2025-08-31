(ns cdq.tx.show-modal
  (:require [cdq.ctx.stage :as stage]))

(defn do! [[_ opts] {:keys [ctx/graphics
                            ctx/stage]}]
  (stage/show-modal-window! stage
                            (:ui-viewport graphics)
                            opts)
  nil)
