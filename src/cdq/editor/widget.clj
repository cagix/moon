(ns cdq.editor.widget
  (:require [cdq.schema :as schema]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn- widget-type
  [schema attribute]
  (let [stype (schema/get-type schema)]
    (cond
     (#{:s/number :s/nat-int :s/int :s/pos :s/pos-int :s/val-max} stype)
     :widget/edn

     :else stype)))

(declare k->methods)

(defn create [schema attribute v ctx]
  (if-let [f (:create (k->methods (widget-type schema attribute)))]
    (f schema attribute v ctx)
    ((:create (k->methods :default)) schema attribute v ctx)))

(defn value [schema attribute widget schemas]
  (if-let [f (:value (k->methods (widget-type schema attribute)))]
    (f schema attribute widget schemas)
    ((:value (k->methods :default)) schema attribute widget schemas)))

(defn build [ctx schema k v]
  (let [widget (actor/build? (create schema k v ctx))]
    (actor/set-user-object! widget [k v])
    widget))
