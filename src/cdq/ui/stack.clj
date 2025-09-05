(ns cdq.ui.stack
  (:require [cdq.ui :as ui]
            cdq.construct
            [cdq.ui.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Stack)))

(defmethod cdq.construct/construct :actor.type/stack [opts]
  (doto (group/proxy-ILookup Stack [])
    (ui/set-opts! opts))) ; TODO group opts already has 'actors' ? stack is a group ?
