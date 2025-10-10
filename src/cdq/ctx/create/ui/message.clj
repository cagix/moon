(ns cdq.ctx.create.ui.message
  (:require [cdq.ui.message :as message]))

(def duration-seconds 0.5)

(defn create [_ctx]
  (message/create duration-seconds))
