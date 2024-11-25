(ns forge.editor.widget
  (:require [clojure.edn :as edn]
            [forge.schema :as schema]
            [forge.ui :as ui]
            [forge.ui.actor :as actor]
            [forge.utils :refer [truncate ->edn-str]])
  (:import (com.kotcrab.vis.ui.widget VisTextField)))

(defn- widget-type [schema _]
  (let [stype (schema/type schema)]
    (cond
     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(defmulti create   widget-type)
(defmulti ->value  widget-type)

(defmethod create :default [_ v]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod ->value :default [_ widget]
  ((actor/id widget) 1))

(defmethod create :widget/edn [schema v]
  (ui/add-tooltip! (ui/text-field (->edn-str v) {})
                   (str schema)))

(defmethod ->value :widget/edn [_ widget]
  (edn/read-string (VisTextField/.getText widget)))
