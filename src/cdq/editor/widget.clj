(ns cdq.editor.widget
  (:require [cdq.schema :as schema]))

(defn- widget-type
  [schema attribute]
  (let [stype (schema/type schema)]
    (cond
     (= attribute :entity/animation)
     :widget/animation

     (= attribute :entity/image)
     :widget/image

     (#{:s/map-optional :s/components-ns} stype)
     :s/map

     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(declare k->methods)

(defn create [schema attribute v ctx]
  (if-let [f (:create (k->methods (widget-type schema attribute)))]
    (f schema attribute v ctx)
    ((:create (k->methods :default )) schema attribute v ctx)))

(defn value [schema attribute widget schemas]
  (if-let [f (:value (k->methods (widget-type schema attribute)))]
    (f schema attribute widget schemas)
    ((:value (k->methods :default)) schema attribute widget schemas)))
