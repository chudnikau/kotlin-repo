package com.sedex.connect.company.pact

import com.sedex.connect.common.Language.en
import com.sedex.connect.common.Language.es
import com.sedex.connect.common.Language.ja
import com.sedex.connect.common.Language.tr
import com.sedex.connect.common.Language.vi
import com.sedex.connect.common.Language.zh
import com.sedex.connect.i18n.MapTranslations

val testTranslations = MapTranslations(
    mapOf(
        en to """
    {
      "ZIC1000000": {
        "text": "A - Agriculture, forestry and fishing"
      },
      "ZIC1010000": {
        "text": "B - Mining and quarrying"
      },
      "ZIC1000100": {
        "text": "Crop and animal production, hunting and related service activities"
      },
      "ZIC1000300": {
        "text": "Fishing and aquaculture"
      },
      "ZIC1000200": {
        "text": "Forestry and logging"
      },
      "ZIC1010200": {
        "text": "Extraction of crude petroleum and natural gas"
      },
      "ZIC1010100": {
        "text": "Mining of coal and lignite"
      },
      "ZIC1010300": {
        "text": "Mining of metal ores"
      },
      "ZIC1010500": {
        "text": "Mining support service activities"
      },
      "ZIC1010400": {
        "text": "Other mining and quarrying"
      }
    }
""",
        es to """
    {
      "ZIC1000000": {
        "text": "A - Agricultura, silvicultura y pesca"
      },
      "ZIC1000100": {
        "text": "Producción agrícola y animal, caza y actividades de servicios conexas"
      },
      "ZIC1000200": {
        "text": "Silvicultura y tala"
      },
      "ZIC1000300": {
        "text": "Pesca y acuicultura"
      },
      "ZIC1010000": {
        "text": "B - Minas y canteras"
      },
      "ZIC1010100": {
        "text": "Minería de carbón y lignito"
      },
      "ZIC1010200": {
        "text": "Extracción de crudo de petróleo y gas natural"
      },
      "ZIC1010300": {
        "text": "Minería de minerales metálicos"
      },
      "ZIC1010400": {
        "text": "Otras explotaciones mineras y canteras"
      },
      "ZIC1010500": {
        "text": "Actividades de servicios de apoyo a la minería"
      }
    }
""",
        zh to """
    {
      "ZIC1000000": {
        "text": "A - 农业、林业和渔业"
      },
      "ZIC1000100": {
        "text": "作物和动物生产、狩猎和相关服务活动"
      },
      "ZIC1000200": {
        "text": "林业和伐木"
      },
      "ZIC1000300": {
        "text": "渔业和水产养殖"
      },
      "ZIC1010000": {
        "text": "B - 采矿和采石"
      },
      "ZIC1010100": {
        "text": "煤炭和褐煤的开采"
      },
      "ZIC1010200": {
        "text": "原油和天然气的开采"
      },
      "ZIC1010300": {
        "text": "金属矿石开采"
      },
      "ZIC1010400": {
        "text": "其他采矿和采石"
      },
      "ZIC1010500": {
        "text": "采矿支持服务活动"
      }
    }
""",
        ja to """
    {
      "ZIC1000000": {
        "text": "A-農業、林業、漁業"
      },
      "ZIC1000100": {
        "text": "作物と動物の生産、狩猟および関連するサービス活動"
      },
      "ZIC1000200": {
        "text": "林業と伐採"
      },
      "ZIC1000300": {
        "text": "釣りと水産養殖"
      },
      "ZIC1010000": {
        "text": "B-鉱業と採石"
      },
      "ZIC1010100": {
        "text": "石炭と亜炭の採掘"
      },
      "ZIC1010200": {
        "text": "原油と天然ガスの抽出"
      },
      "ZIC1010300": {
        "text": "金属鉱石の採掘"
      },
      "ZIC1010400": {
        "text": "その他の採掘および採石"
      },
      "ZIC1010500": {
        "text": "マイニングサポートサービス活動"
      }
    }
""",
        tr to """
    {
      "ZIC1000000": {
        "text": "A - Tarım, ormancılık ve balıkçılık"
      },
      "ZIC1000100": {
        "text": "Tahıl ve hayvansal üretim, avcılık ve ilgili hizmet faaliyetleri"
      },
      "ZIC1000200": {
        "text": "Ormancılık ve Ormancılık"
      },
      "ZIC1000300": {
        "text": "Balıkçılık ve su ürünleri yetiştiriciliği"
      },
      "ZIC1010000": {
        "text": "B-Madencilik ve Taşocakçılığı"
      },
      "ZIC1010100": {
        "text": "Kömür ve linyit madenciliği"
      },
      "ZIC1010200": {
        "text": "Ham petrol ve doğal gazın çıkarılması"
      },
      "ZIC1010300": {
        "text": "Metal Cevheri Madenciliği"
      },
      "ZIC1010400": {
        "text": "Diğer madencilik ve taşocakçılığı"
      },
      "ZIC1010500": {
        "text": "Madencilik destek hizmeti etkinliği"
      }
    }
""",
        vi to """
    {
       "ZIC1000000": {
         "text": "A-Nông nghiệp, lâm nghiệp, thủy sản"
       },
       "ZIC1000100": {
         "text": "Trồng trọt và chăn nuôi, săn bắn và các hoạt động dịch vụ có liên quan"
       },
       "ZIC1000200": {
         "text": "Lâm nghiệp và Khai thác gỗ"
       },
       "ZIC1000300": {
         "text": "Đánh bắt và nuôi trồng thủy sản"
       },
       "ZIC1010000": {
         "text": "B-Mining and Quarrying"
       },
       "ZIC1010100": {
         "text": "Khai thác than và than non"
       },
       "ZIC1010200": {
         "text": "Khai thác dầu thô và khí tự nhiên"
       },
       "ZIC1010300": {
         "text": "Khai thác quặng kim loại"
       },
       "ZIC1010400": {
         "text": "Khai thác và khai thác đá khác"
       },
       "ZIC1010500": {
         "text": "Hoạt động dịch vụ hỗ trợ khai thác"
       }
     }
""",
    )
)
