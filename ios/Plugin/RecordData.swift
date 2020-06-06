//
//  Created by Avihu Harush on 20/01/2020
//

import Foundation

struct RecordData {
    public let recordDataBase64: String?
    
    public func toDictionary() -> Dictionary<String, Any> {
        return [
            "recordDataBase64": recordDataBase64 ?? ""
        ]
    }
}
