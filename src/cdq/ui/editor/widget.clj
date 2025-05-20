(ns cdq.ui.editor.widget
  (:require [cdq.schema :as schema]))

(defn- widget-type [schema]
  (let [stype (schema/type schema)]
    (cond
     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(defmulti create (fn [schema _v _ctx]
                   (widget-type schema)))
(defmulti value  (fn [schema _v _schemas]
                   (widget-type schema)))
