(ns clojure.start
  (:require cdq.effect
            cdq.schema
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.object :as object]
            [clojure.walk :as walk])
  (:gen-class))

(def effect-fn-map
  '{:effects/audiovisual {:applicable? cdq.effects.audiovisual/applicable?
                          :useful? cdq.effects.audiovisual/useful?
                          :handle cdq.effects.audiovisual/handle}
    :effects/projectile {:applicable? cdq.effects.projectile/applicable?
                         :useful? cdq.effects.projectile/useful?
                         :handle cdq.effects.projectile/handle}
    :effects/sound {:applicable? cdq.effects.sound/applicable?
                    :useful? cdq.effects.sound/useful?
                    :handle cdq.effects.sound/handle}
    :effects/spawn {:applicable? cdq.effects.spawn/applicable?
                    :handle cdq.effects.spawn/handle}
    :effects/target-all {:applicable? cdq.effects.target-all/applicable?
                         :useful? cdq.effects.target-all/useful?
                         :handle cdq.effects.target-all/handle
                         :render cdq.effects.target-all/render}
    :effects/target-entity {:applicable? cdq.effects.target-entity/applicable?
                            :useful? cdq.effects.target-entity/useful?
                            :handle cdq.effects.target-entity/handle
                            :render cdq.effects.target-entity/render}
    :effects.target/audiovisual {:applicable? cdq.effects.target.audiovisual/applicable?
                                 :useful? cdq.effects.target.audiovisual/useful?
                                 :handle cdq.effects.target.audiovisual/handle}
    :effects.target/convert {:applicable? cdq.effects.target.convert/applicable?
                             :handle cdq.effects.target.convert/handle}
    :effects.target/damage {:applicable? cdq.effects.target.damage/applicable?
                            :handle cdq.effects.target.damage/handle}
    :effects.target/kill {:applicable? cdq.effects.target.kill/applicable?
                          :handle cdq.effects.target.kill/handle}
    :effects.target/melee-damage {:applicable? cdq.effects.target.melee-damage/applicable?
                                  :handle cdq.effects.target.melee-damage/handle}
    :effects.target/spiderweb {:applicable? cdq.effects.target.spiderweb/applicable?
                               :handle      cdq.effects.target.spiderweb/handle}
    :effects.target/stun {:applicable? cdq.effects.target.stun/applicable?
                          :handle      cdq.effects.target.stun/handle}})

(def schema-fn-map
  '{:s/animation {:malli-form cdq.schema.animation/malli-form
                  :create cdq.schema.animation/create
                  :value cdq.ui.editor.widget.default/value}

    :s/boolean {:malli-form cdq.schema.boolean/malli-form
                :create cdq.schema.boolean/create
                :value cdq.schema.boolean/value}

    :s/enum {:malli-form cdq.schema.enum/malli-form
             :create cdq.schema.enum/create
             :value cdq.schema.enum/value}

    :s/image {:malli-form cdq.schema.image/malli-form
              :create cdq.schema.image/create
              :value cdq.ui.editor.widget.default/value}

    :s/map {:malli-form cdq.schema.map/malli-form
            :create cdq.schema.map/create
            :value cdq.schema.map/value}

    :s/number {:malli-form cdq.schema.number/malli-form
               :create cdq.ui.editor.widget.edn/create
               :value cdq.ui.editor.widget.edn/value}

    :s/one-to-many {:malli-form cdq.schema.one-to-many/malli-form
                    :create-value cdq.schema.one-to-many/create-value
                    :create cdq.schema.one-to-many/create
                    :value cdq.schema.one-to-many/value}

    :s/one-to-one {:malli-form cdq.schema.one-to-one/malli-form
                   :create-value cdq.schema.one-to-one/create-value
                   :create cdq.schema.one-to-one/create
                   :value cdq.schema.one-to-one/value}

    :s/qualified-keyword {:malli-form cdq.schema.qualified-keyword/malli-form
                          :create cdq.ui.editor.widget.default/create
                          :value cdq.ui.editor.widget.default/value}

    :s/some {:malli-form cdq.schema.some/malli-form
             :create cdq.ui.editor.widget.default/create
             :value cdq.ui.editor.widget.default/value}

    :s/sound {:malli-form cdq.schema.sound/malli-form
              :create cdq.schema.sound/create
              :value cdq.ui.editor.widget.default/value}

    :s/string {:malli-form cdq.schema.string/malli-form
               :create cdq.schema.string/create
               :value cdq.schema.string/value}

    :s/val-max {:malli-form cdq.schema.val-max/malli-form
                :create cdq.ui.editor.widget.edn/create
                :value cdq.ui.editor.widget.edn/value}

    :s/vector {:malli-form cdq.schema.vector/malli-form
               :create cdq.ui.editor.widget.default/create
               :value cdq.ui.editor.widget.default/value}
    })

(defn require-resolve-symbols [form]
  (walk/postwalk (fn [form]
                   (if (symbol? form)
                     (let [var (requiring-resolve form)]
                       (assert var form)
                       var)
                     form))
                 form))

(defn -main []
  (.bindRoot #'cdq.schema/k->methods    (require-resolve-symbols schema-fn-map))
  (.bindRoot #'cdq.effect/k->method-map (require-resolve-symbols effect-fn-map))
  (-> "clojure.start.edn"
      io/resource
      slurp
      edn/read-string
      require-resolve-symbols
      object/pipeline))
