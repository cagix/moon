(ns cdq.editor.widgets
  (:require [cdq.walk :as walk]))

(def k->methods*
  '{:s/animation {:malli-form cdq.schema.animation/malli-form
                  :create cdq.schema.animation/create
                  :value cdq.editor.widget.default/value}

    :s/boolean {:malli-form cdq.schema.boolean/malli-form
                :create cdq.schema.boolean/create
                :value cdq.schema.boolean/value}

    :s/enum {:malli-form cdq.schema.enum/malli-form
             :create cdq.schema.enum/create
             :value cdq.schema.enum/value}

    :s/image {:malli-form cdq.schema.image/malli-form
              :create cdq.schema.image/create
              :value cdq.editor.widget.default/value}

    :s/map {:malli-form cdq.schema.map/malli-form
            :create cdq.schema.map/create
            :value cdq.schema.map/value}

    :s/number {:malli-form cdq.schema.number/malli-form
               :create cdq.editor.widget.edn/create
               :value cdq.editor.widget.edn/value}

    :s/one-to-many {:malli-form cdq.schema.one-to-many/malli-form
                    :create-value cdq.schema.one-to-many/create-value
                    :create cdq.schema.one-to-many/create
                    :value cdq.schema.one-to-many/value}

    :s/one-to-one {:malli-form cdq.schema.one-to-one/malli-form
                   :create-value cdq.schema.one-to-one/create-value
                   :create cdq.schema.one-to-one/create
                   :value cdq.schema.one-to-one/value}

    :s/qualified-keyword {:malli-form cdq.schema.qualified-keyword/malli-form
                          :create cdq.editor.widget.default/create
                          :value cdq.editor.widget.default/value}

    :s/some {:malli-form cdq.schema.some/malli-form
             :create cdq.editor.widget.default/create
             :value cdq.editor.widget.default/value}

    :s/sound {:malli-form cdq.schema.sound/malli-form
              :create cdq.schema.sound/create
              :value cdq.editor.widget.default/value}

    :s/string {:malli-form cdq.schema.string/malli-form
               :create cdq.schema.string/create
               :value cdq.schema.string/value}

    :s/val-max {:malli-form cdq.schema.val-max/malli-form
                :create cdq.editor.widget.edn/create
                :value cdq.editor.widget.edn/value}

    :s/vector {:malli-form cdq.schema.vector/malli-form
               :create cdq.editor.widget.default/create
               :value cdq.editor.widget.default/value}
    })

(def k->methods
  (walk/require-resolve-symbols k->methods*))
