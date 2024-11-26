(ns forge.editor.widget
  (:require [clojure.edn :as edn]
            [forge.db :as db]
            [forge.ui :as ui]
            [forge.ui.actor :as actor]
            [forge.utils :refer [truncate ->edn-str]])
  (:import (com.kotcrab.vis.ui.widget VisCheckBox VisTextField VisSelectBox)))

(defn- widget-type [schema _]
  (let [stype (db/schema-type schema)]
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

(defmethod create :string [schema v]
  (ui/add-tooltip! (ui/text-field v {})
                   (str schema)))

(defmethod ->value :string [_ widget]
  (VisTextField/.getText widget))

(defmethod create :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod ->value :boolean [_ widget]
  (VisCheckBox/.isChecked widget))

(defmethod create :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod ->value :enum [_ widget]
  (edn/read-string (VisSelectBox/.getSelected widget)))
