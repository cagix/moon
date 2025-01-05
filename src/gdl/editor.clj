(ns gdl.editor
  (:require [gdl.app :as app]))

(def ^:private config
  {:config {:title "Editor"
            :fps 60
            :width 1440
            :height 900
            :taskbar-icon "icon.png"} ; TODO and 'name' is still 'JAVA'
   :context [[:gdl.context/unit-scale 1]
             [:gdl.context/batch]

             ; TODO need test DB
             ;
             [:gdl.context/db {:schema "schema.edn"
                               :properties "properties.edn"}]
             ;;

             [:gdl.context/assets "resources/"]
             [:gdl.context/viewport {:width 1440 :height 900}]

             ;; just because of sprite edn->value of db requires world-unit-scale
             [:gdl.context/world-unit-scale 1]
             [:gdl.context/world-viewport {:width 1440 :height 900}]
             ;;

             [:gdl.context/ui :skin-scale/x1]
             [:gdl.context/stage]
             [:gdl.editor/actors]]
   :transactions '[gdl.context/update-stage
                   gdl.graphics/draw-stage]})

(defn -main []
  (app/start config))
