(ns cdq.ui.horizontal-group
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [cdq.ui.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui HorizontalGroup)))

(defmethod actor/construct :actor.type/horizontal-group [{:keys [space pad] :as opts}]
  (let [group (group/proxy-ILookup HorizontalGroup [])]
    (when space (.space group (float space)))
    (when pad   (.pad   group (float pad)))
    (ui/set-opts! group opts)))
