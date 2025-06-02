(ns clojure.tx.show-message
  (:require [clojure.ctx.effect-handler :refer [do!]]
            [clojure.ui.message]
            [clojure.ui.stage :as stage]))

(defmethod do! :tx/show-message [[_ message]
                                 {:keys [ctx/stage]}]
  (-> stage
      (stage/find-actor "player-message")
      (clojure.ui.message/show! message)))
