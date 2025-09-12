(ns clojure.start
  (:require cdq.effect
            cdq.ctx.db
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.object :as object]
            [clojure.walk :as walk]
            clojure.provide
            clojure.scene2d.stage
            clojure.scene2d.input-event)
  (:gen-class))

(clojure.provide/do!
 [[com.badlogic.gdx.scenes.scene2d.Stage
   'clojure.gdx.scenes.scene2d.stage
   clojure.scene2d.stage/Stage]
  [com.badlogic.gdx.scenes.scene2d.InputEvent
   'clojure.gdx.scenes.scene2d.input-event
   clojure.scene2d.input-event/InputEvent]
  ])

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

(defn return-identity [_ v _db]
  v)

(defn convert-fn-map [fn-map]
  (into {} (for [[proto-sym impl-sym] fn-map]
             [proto-sym (requiring-resolve impl-sym)])))

(def schema-fn-map
  '{:s/animation {cdq.schema/malli-form cdq.schema.animation/malli-form
                  cdq.schema/create-value clojure.start/return-identity
                  cdq.schema/create cdq.schema.animation/create
                  cdq.schema/value cdq.ui.editor.widget.default/value}

    :s/boolean {cdq.schema/malli-form cdq.schema.boolean/malli-form
                cdq.schema/create-value clojure.start/return-identity
                cdq.schema/create cdq.schema.boolean/create
                cdq.schema/value cdq.schema.boolean/value}

    :s/enum {cdq.schema/malli-form cdq.schema.enum/malli-form
             cdq.schema/create-value clojure.start/return-identity
             cdq.schema/create cdq.schema.enum/create
             cdq.schema/value cdq.schema.enum/value}

    :s/image {cdq.schema/malli-form cdq.schema.image/malli-form
              cdq.schema/create-value clojure.start/return-identity
              cdq.schema/create cdq.schema.image/create
              cdq.schema/value cdq.ui.editor.widget.default/value}

    :s/map {cdq.schema/malli-form cdq.schema.map/malli-form
            cdq.schema/create-value clojure.start/return-identity
            cdq.schema/create cdq.schema.map/create
            cdq.schema/value cdq.schema.map/value}

    :s/number {cdq.schema/malli-form cdq.schema.number/malli-form
               cdq.schema/create-value clojure.start/return-identity
               cdq.schema/create cdq.ui.editor.widget.edn/create
               cdq.schema/value cdq.ui.editor.widget.edn/value}

    :s/one-to-many {cdq.schema/malli-form cdq.schema.one-to-many/malli-form
                    cdq.schema/create-value cdq.schema.one-to-many/create-value
                    cdq.schema/create cdq.schema.one-to-many/create
                    cdq.schema/value cdq.schema.one-to-many/value}

    :s/one-to-one {cdq.schema/malli-form cdq.schema.one-to-one/malli-form
                   cdq.schema/create-value cdq.schema.one-to-one/create-value
                   cdq.schema/create cdq.schema.one-to-one/create
                   cdq.schema/value cdq.schema.one-to-one/value}

    :s/qualified-keyword {cdq.schema/malli-form cdq.schema.qualified-keyword/malli-form
                          cdq.schema/create-value clojure.start/return-identity
                          cdq.schema/create cdq.ui.editor.widget.default/create
                          cdq.schema/value cdq.ui.editor.widget.default/value}

    :s/some {cdq.schema/malli-form cdq.schema.some/malli-form
             cdq.schema/create-value clojure.start/return-identity
             cdq.schema/create cdq.ui.editor.widget.default/create
             cdq.schema/value cdq.ui.editor.widget.default/value}

    :s/sound {cdq.schema/malli-form cdq.schema.sound/malli-form
              cdq.schema/create-value clojure.start/return-identity
              cdq.schema/create cdq.schema.sound/create
              cdq.schema/value cdq.ui.editor.widget.default/value}

    :s/string {cdq.schema/malli-form cdq.schema.string/malli-form
               cdq.schema/create-value clojure.start/return-identity
               cdq.schema/create cdq.schema.string/create
               cdq.schema/value cdq.schema.string/value}

    :s/val-max {cdq.schema/malli-form cdq.schema.val-max/malli-form
                cdq.schema/create-value clojure.start/return-identity
                cdq.schema/create cdq.ui.editor.widget.edn/create
                cdq.schema/value cdq.ui.editor.widget.edn/value}

    :s/vector {cdq.schema/malli-form cdq.schema.vector/malli-form
               cdq.schema/create-value clojure.start/return-identity
               cdq.schema/create cdq.ui.editor.widget.default/create
               cdq.schema/value cdq.ui.editor.widget.default/value}
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
  (.bindRoot #'cdq.ctx.db/schema-fn-map
             (into {} (for [[k fn-map] schema-fn-map]
                        [k (convert-fn-map fn-map)])))
  (.bindRoot #'cdq.effect/k->method-map (require-resolve-symbols effect-fn-map))
  (-> "clojure.start.edn"
      io/resource
      slurp
      edn/read-string
      require-resolve-symbols
      object/pipeline))
