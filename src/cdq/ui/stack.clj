(ns cdq.ui.stack
  (:require [cdq.ui :as ui]
            cdq.construct
            [clojure.gdx.scenes.scene2d.ui.stack :as stack]))

(defmethod cdq.construct/construct :actor.type/stack [opts]
  (doto (stack/create)
    (ui/set-opts! opts))) ; TODO group opts already has 'actors' ? stack is a group ?
