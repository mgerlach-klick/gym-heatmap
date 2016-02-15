(ns gymheatmap.core
  (:gen-class)
  (:require [clj-time
             [core :as t]
             [predicates :as p]
             [format :as f]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [seesaw.core :refer :all])
  (:import org.tc33.jheatchart.HeatChart))

;; 2/2/2015 17:19
(def dusk-till-dawn (for [h (range 0 24)
                          m [0 30]]
                      (str h ":" m)))

(defn heat-chart
  ([matrix]
   (HeatChart. (into-array (map double-array matrix))))
  ([matrix & {:keys [title x-label y-label] :or {title "Heat-Chart"
                                                 x-label "X"
                                                 y-label "Y"} :as opts}]
   (doto (heat-chart matrix)
     (.setTitle title)
     (.setXAxisLabel x-label)
     (.setYAxisLabel y-label))))

(defn day-time-heat-chart [matrix]
  (doto (heat-chart matrix :title "Gym Swipes" :x-label "Time" :y-label "Day of Week")
    (.setYValues (to-array ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"]))
    (.setXValues (to-array dusk-till-dawn))))


(defn save-heat-chart [heat-chart path]
  (.saveToFile heat-chart (io/file path)))

(defn show-heat-chart [heat-chart]
  (->
   (frame
    :title "Graph"
    :content (label :icon (.getChartImage heat-chart)))
   pack!
   show!))


(defn slurp-resource [name]
  (slurp
   (io/file
    (io/resource
     name))))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn parse-datetime [dt]
  (let [custom-formatter (f/formatter "M/d/yyyy H:mm")]
    (f/parse custom-formatter dt)))

(def dt (parse-datetime  "2/1/2016 20:53"))

(defn round-to-nearest-half-hour [dt]
  (let [minutes (t/minute dt)]
    (cond
      (< minutes 15) (t/minus dt (t/minutes minutes))
      (< minutes 30) (t/plus dt (t/minutes (- 30 minutes)))
      (< minutes 45) (t/minus dt (t/minutes (- minutes 30)))
      :leq-60 (t/plus dt (t/minutes (- 60 minutes))))))


(def gym-swipes (slurp-resource "gyminout.csv"))

(defn parse-swipes [swipes]
  (->> (csv/read-csv swipes)
       (filter (fn [[_ in-or-out _]] (= "In" in-or-out)))
       (map (fn [[_ _ dt]]
              (-> dt
                  parse-datetime
                  round-to-nearest-half-hour)))))

(defn gym-occupancy
  "Assume people stay in the gym for an hour"
  [swipe-ins]
  (mapcat (fn [swipe-in] [swipe-in
                         (t/plus swipe-in (t/minutes 30))
                         (t/plus swipe-in (t/minutes 60))])
          swipe-ins))

(defn swipes-by-weekday [swipes]
  (group-by t/day-of-week swipes))

(defn as-hour-str
  "joda time to hour string"
  [dt]
  (str (t/hour dt) ":" (t/minute dt)))

(defn map-to-times
  "Map from simple swipes to z-values across a 24 hour scale in 30min intervals"
  [swipes]
  (let [vals dusk-till-dawn
        empty-z-vals (reduce (fn [acc val]
                         (assoc acc val 0)) {} vals)]
    (reduce (fn [acc swipe]
              (update acc (as-hour-str swipe) inc)) empty-z-vals swipes)))

(defn heatchart-format-array
  "takes a map of weekday (as int) to map of time to swipe-ins and produces a heat-chart that visualizes it"
  [dow]
  (vec (for [day (range 1 8)]
         (let [day-swipes (get dow day)]
           (mapv (fn [time] (get day-swipes time)) dusk-till-dawn)))))


(defn swipe-in-count []
  (->> (-> gym-swipes
           parse-swipes
           gym-occupancy
           swipes-by-weekday)
       (map (fn [[day swipes]]            ;gives me the swipe-in frequency by DoW
              [day (map-to-times swipes)]))
       (into (hash-map))
       heatchart-format-array
      day-time-heat-chart))

